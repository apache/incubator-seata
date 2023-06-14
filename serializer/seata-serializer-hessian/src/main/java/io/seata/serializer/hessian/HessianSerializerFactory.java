/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.serializer.hessian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.caucho.hessian.io.SerializerFactory;

/*
 * @Xin Wang
 */
public class HessianSerializerFactory extends SerializerFactory {
    public static final SerializerFactory INSTANCE = new HessianSerializerFactory();

    private HessianSerializerFactory() {
        super();
        super.getClassFactory().setWhitelist(true);
        super.getClassFactory().allow("io.seata.*");
        allowBasicTypes();
        allowCollections();
        denyTypes();
    }

    public static SerializerFactory getInstance() {
        return INSTANCE;
    }

    private void allowBasicTypes() {
        super.getClassFactory().allow(boolean.class.getCanonicalName());
        super.getClassFactory().allow(byte.class.getCanonicalName());
        super.getClassFactory().allow(char.class.getCanonicalName());
        super.getClassFactory().allow(double.class.getCanonicalName());
        super.getClassFactory().allow(float.class.getCanonicalName());
        super.getClassFactory().allow(int.class.getCanonicalName());
        super.getClassFactory().allow(long.class.getCanonicalName());
        super.getClassFactory().allow(short.class.getCanonicalName());
        super.getClassFactory().allow(Boolean.class.getCanonicalName());
        super.getClassFactory().allow(Byte.class.getCanonicalName());
        super.getClassFactory().allow(Character.class.getCanonicalName());
        super.getClassFactory().allow(Double.class.getCanonicalName());
        super.getClassFactory().allow(Float.class.getCanonicalName());
        super.getClassFactory().allow(Integer.class.getCanonicalName());
        super.getClassFactory().allow(Long.class.getCanonicalName());
        super.getClassFactory().allow(Short.class.getCanonicalName());

        super.getClassFactory().allow(Number.class.getCanonicalName());
        super.getClassFactory().allow(Class.class.getCanonicalName());
        super.getClassFactory().allow(String.class.getCanonicalName());
    }

    private void allowCollections() {
        super.getClassFactory().allow(List.class.getCanonicalName());
        super.getClassFactory().allow(ArrayList.class.getCanonicalName());
        super.getClassFactory().allow(LinkedList.class.getCanonicalName());

        super.getClassFactory().allow(Set.class.getCanonicalName());
        super.getClassFactory().allow(HashSet.class.getCanonicalName());
        super.getClassFactory().allow(LinkedHashSet.class.getCanonicalName());
        super.getClassFactory().allow(TreeSet.class.getCanonicalName());

        super.getClassFactory().allow(Map.class.getCanonicalName());
        super.getClassFactory().allow(HashMap.class.getCanonicalName());
        super.getClassFactory().allow(LinkedHashMap.class.getCanonicalName());
        super.getClassFactory().allow(TreeMap.class.getCanonicalName());
    }

    private void denyTypes(){
        super.getClassFactory().deny("javax.naming.InitialContext");
        super.getClassFactory().deny("javax.net.ssl.*");
        super.getClassFactory().deny("com.unboundid.ldap.*");
    }
}
