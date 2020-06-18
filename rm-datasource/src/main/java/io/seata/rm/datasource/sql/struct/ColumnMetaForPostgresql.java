package io.seata.rm.datasource.sql.struct;
/**
 * @author yanyujie
 * @date 2020年6月18日09:24:191
 * 将charOctetLength这个字段int改为String  解决postgresql的驱动兼容性问题
 */
public class ColumnMetaForPostgresql extends ColumnMeta {


    private String charOctetLengthTopg;

    public String getCharOctetLengthTopg() {
        return charOctetLengthTopg;
    }

    public void setCharOctetLengthTopg(String charOctetLengthTopg) {
        this.charOctetLengthTopg = charOctetLengthTopg;
    }
}
