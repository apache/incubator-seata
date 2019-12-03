package io.seata.codec.hessian;

import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;
import com.caucho.hessian.io.SerializerFactory;

/*
 * @Xin Wang
 */
public class HessianSerializerFactory extends SerializerFactory {
    public static final SerializerFactory INSTANCE = new HessianSerializerFactory();

    private HessianSerializerFactory() {
        super();
    }

    public static SerializerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    protected Serializer loadSerializer(Class<?> cl) throws HessianProtocolException {
        return super.loadSerializer(cl);
    }

    @Override
    protected Deserializer loadDeserializer(Class cl) throws HessianProtocolException {
        return super.loadDeserializer(cl);
    }
}
