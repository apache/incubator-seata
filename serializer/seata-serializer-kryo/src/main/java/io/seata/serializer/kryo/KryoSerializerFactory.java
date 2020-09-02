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
package io.seata.serializer.kryo;

import java.lang.reflect.InvocationHandler;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Arrays;
import java.util.BitSet;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.regex.Pattern;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.BitSetSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.RegexSerializer;
import de.javakaffee.kryoserializers.URISerializer;
import de.javakaffee.kryoserializers.UUIDSerializer;
import io.seata.core.serializer.SerializerClassRegistry;

/**
 * @author jsbxyyx
 */
public class KryoSerializerFactory implements KryoFactory {

    private static final KryoSerializerFactory FACTORY = new KryoSerializerFactory();

    private KryoPool pool = new KryoPool.Builder(this).softReferences().build();

    private KryoSerializerFactory() {}

    public static KryoSerializerFactory getInstance() {
        return FACTORY;
    }

    public KryoInnerSerializer get() {
        return new KryoInnerSerializer(pool.borrow());
    }

    public void returnKryo(KryoInnerSerializer kryoSerializer) {
        if (kryoSerializer == null) {
            throw new IllegalArgumentException("kryoSerializer is null");
        }
        pool.release(kryoSerializer.getKryo());
    }

    @Override
    public Kryo create() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);

        // register serializer
        kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
        kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
        kryo.register(InvocationHandler.class, new JdkProxySerializer());
        kryo.register(BigDecimal.class, new DefaultSerializers.BigDecimalSerializer());
        kryo.register(BigInteger.class, new DefaultSerializers.BigIntegerSerializer());
        kryo.register(Pattern.class, new RegexSerializer());
        kryo.register(BitSet.class, new BitSetSerializer());
        kryo.register(URI.class, new URISerializer());
        kryo.register(UUID.class, new UUIDSerializer());

        // register commonly class
        SerializerClassRegistry.getRegisteredClasses().keySet().forEach(kryo::register);
        return kryo;
    }

}
