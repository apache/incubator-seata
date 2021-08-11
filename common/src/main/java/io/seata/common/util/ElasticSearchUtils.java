package io.seata.common.util;

import io.seata.common.exception.NotSupportYetException;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;


/**
 * @author UmizzZ
 * @date
 */
public class ElasticSearchUtils {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchUtils.class);

    public ElasticSearchUtils(){

    }

    public static Map<String, Object> toESjsonMap(Object object, String indexId){
        if(object == null){
            return null;
        }
        Map<String, Object> map = new HashMap<>(16);
        Field[] fields = object.getClass().getDeclaredFields();
        ArrayList<Field> fieldArrayList = new ArrayList<>(Arrays.asList(fields));
        for(int i = 0;i<fieldArrayList.size(); i++){
            if(fieldArrayList.get(i).getName() == indexId){
                fieldArrayList.remove(i);
            }
        }
        try{
            for (Field field : fieldArrayList) {
                boolean accessible = field.isAccessible();
                field.setAccessible(true);
                map.put(field.getName(), field.get(object));
                field.setAccessible(accessible);
            }
        }
        catch (IllegalAccessException e){
            throw new NotSupportYetException(
                    "object" + object.getClass().toString() + "to map failed" + e.getMessage()
            );
        }
        return map;
    }

    //ObjectMap To StringMap
    public static Map<String, String> toStringMap(Map<String, Object> stringObjectMap){
        Map<String, String> result = new HashMap<>();
        Iterator iterator = stringObjectMap.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry entry = (Map.Entry) iterator.next();
            if (entry.getValue() != null && entry.getKey()!=null) {
                //08-05修改
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

}
