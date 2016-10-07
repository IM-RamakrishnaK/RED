/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table;

import java.util.LinkedHashMap;
import java.util.Map;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.ModelType;


public class SettingTableElementsComparator extends
        AModelTypeComparator<AModelElement<SettingTable>> {

    private final static Map<ModelType, Integer> position = new LinkedHashMap<>();
    static {
        int startPosition = 1;
        position.put(ModelType.SUITE_DOCUMENTATION, startPosition);
        position.put(ModelType.SUITE_SETUP, ++startPosition);
        position.put(ModelType.SUITE_TEARDOWN, ++startPosition);
        position.put(ModelType.SUITE_TEST_SETUP, ++startPosition);
        position.put(ModelType.SUITE_TEST_TEARDOWN, ++startPosition);
        position.put(ModelType.FORCE_TAGS_SETTING, ++startPosition);
        position.put(ModelType.DEFAULT_TAGS_SETTING, ++startPosition);
        position.put(ModelType.SUITE_TEST_TEMPLATE, ++startPosition);
        position.put(ModelType.SUITE_TEST_TIMEOUT, ++startPosition);
        position.put(ModelType.METADATA_SETTING, ++startPosition);
        position.put(ModelType.LIBRARY_IMPORT_SETTING, ++startPosition);
        position.put(ModelType.RESOURCE_IMPORT_SETTING, ++startPosition);
        position.put(ModelType.RESOURCE_IMPORT_SETTING, ++startPosition);
    }


    public SettingTableElementsComparator() {
        super(position);
    }
}
