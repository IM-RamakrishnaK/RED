/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testData.text.read.recognizer.userKeywords;

import org.rf.ide.core.testData.text.read.recognizer.AExecutableElementSettingsRecognizer;
import org.rf.ide.core.testData.text.read.recognizer.ATokenRecognizer;
import org.rf.ide.core.testData.text.read.recognizer.RobotTokenType;


public class KeywordTeardownRecognizer extends
        AExecutableElementSettingsRecognizer {

    public KeywordTeardownRecognizer() {
        super(RobotTokenType.KEYWORD_SETTING_TEARDOWN);
    }


    @Override
    public ATokenRecognizer newInstance() {
        return new KeywordTeardownRecognizer();
    }
}
