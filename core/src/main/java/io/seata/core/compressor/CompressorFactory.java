package io.seata.core.compressor;

import io.seata.common.loader.EnhancedServiceLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * the type compressor factory
 * @author jsbxyyx
 */
public class CompressorFactory {

    /**
     * The constant COMPRESSOR_MAP.
     */
    protected static final Map<CompressorType, Compressor> COMPRESSOR_MAP = new ConcurrentHashMap<CompressorType, Compressor>();

    /**
     * Get compressor by code.
     *
     * @param code the code
     * @return the compressor
     */
    public static synchronized Compressor getCompressor(byte code) {
        CompressorType type = CompressorType.getByCode(code);
        if (COMPRESSOR_MAP.get(type) != null) {
            return COMPRESSOR_MAP.get(type);
        }
        Compressor impl = EnhancedServiceLoader.load(Compressor.class, type.name());
        COMPRESSOR_MAP.put(type, impl);
        return impl;
    }

}
