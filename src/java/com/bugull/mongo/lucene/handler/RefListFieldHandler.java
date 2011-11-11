/**
 * Copyright (c) www.bugull.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bugull.mongo.lucene.handler;

import com.bugull.mongo.BuguDao;
import com.bugull.mongo.BuguEntity;
import com.bugull.mongo.cache.DaoCache;
import com.bugull.mongo.cache.FieldsCache;
import com.bugull.mongo.lucene.annotations.IndexRefBy;
import com.bugull.mongo.mapper.Operator;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.apache.lucene.document.Document;
import org.bson.types.ObjectId;

/**
 *
 * @author Frank Wen(xbwen@hotmail.com)
 */
public class RefListFieldHandler extends AbstractFieldHandler{
    
    public RefListFieldHandler(Object obj, Field field, String prefix){
        super(obj, field, prefix);
    }

    @Override
    public void handle(Document doc) throws Exception{
        Class clazz = null;
        Object value = field.get(obj);
        Class<?> type = field.getType();
        ObjectId[] ids = null;
        if(type.isArray()){
            clazz = type.getComponentType();
            int len = Array.getLength(value);
            ids = new ObjectId[len];
            for(int i=0; i<len; i++){
                BuguEntity entity = (BuguEntity)Array.get(value, i);
                ids[i] = new ObjectId(entity.getId());
            }
        }else{
            ParameterizedType paramType = (ParameterizedType)field.getGenericType();
            Type[] types = paramType.getActualTypeArguments();
            if(types.length == 1){
                clazz = (Class)types[0];
                List<BuguEntity> li = (List<BuguEntity>)value;
                int size = li.size();
                ids = new ObjectId[size];
                for(int i=0; i<size; i++){
                    ids[i] = new ObjectId(li.get(i).getId());
                }
            }else{
                return;  //do not support Map now
            }
        }
        BuguDao dao = DaoCache.getInstance().get(clazz);
        DBObject in = new BasicDBObject(Operator.IN, ids);
        DBObject query = new BasicDBObject(Operator.ID, in);
        List list = dao.find(query);
        if(list!=null && list.size()>0){
            Field[] fields = FieldsCache.getInstance().get(clazz);
            for(Field f : fields){
                IndexRefBy irb = f.getAnnotation(IndexRefBy.class);
                if(irb != null){
                    FieldHandler handler = new RefByFieldHandler(obj.getClass(), list, f, prefix);
                    handler.handle(doc);
                }
            }
        }
    }
    
}