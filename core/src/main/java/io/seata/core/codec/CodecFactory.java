package io.seata.core.codec;

import io.seata.common.loader.EnhancedServiceLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Codec factory.
 *
 * @author zhangsen
 * @data 2019 /5/6
 */
public class CodecFactory {

    /**
     * The constant CODEC_MAP.
     */
    protected static final Map<CodecType, Codec> CODEC_MAP = new ConcurrentHashMap<CodecType, Codec>();

    /**
     * Get codec codec.
     *
     * @param code the code
     * @return the codec
     */
    public static synchronized Codec getCodec(byte code){
        CodecType codecType = CodecType.getResultCode(code);
        if(CODEC_MAP.get(codecType) != null){
            return CODEC_MAP.get(codecType);
        }
        Codec codec = EnhancedServiceLoader.load(Codec.class, codecType.name());
        CODEC_MAP.put(codecType, codec);
        return codec;
    }

    /**
     * Encode byte [ ].
     *
     * @param <T>  the type parameter
     * @param code the code
     * @param t    the t
     * @return the byte [ ]
     */
    public static <T> byte[] encode(byte code, T t){
        return getCodec(code).encode(t);
    }

    /**
     * Decode t.
     *
     * @param <T>   the type parameter
     * @param code  the code
     * @param bytes the bytes
     * @return the t
     */
    public static <T> T decode(byte code, byte[] bytes){
        return getCodec(code).decode(bytes);
    }


}
