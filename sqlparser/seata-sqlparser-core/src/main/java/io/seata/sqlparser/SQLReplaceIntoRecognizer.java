package io.seata.sqlparser;

import java.util.List;

public interface SQLReplaceIntoRecognizer extends SQLRecognizer {
    boolean selectQueryIsEmpty();

    List<String> getReplaceColumns();

    List<String> getReplaceValues();

    String getSelectQuery();
}
