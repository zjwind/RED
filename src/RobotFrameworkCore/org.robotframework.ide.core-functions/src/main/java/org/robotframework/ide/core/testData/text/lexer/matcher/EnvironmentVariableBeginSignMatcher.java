package org.robotframework.ide.core.testData.text.lexer.matcher;

import org.robotframework.ide.core.testData.text.lexer.RobotSingleCharTokenType;


/**
 * Matcher responsible for handling {@code PROCENT}.
 * 
 * @author wypych
 * @since JDK 1.7 update 74
 * @version Robot Framework 2.9 alpha 2
 * 
 * @see RobotTokenMatcher
 * @see RobotSingleCharTokenType#SINGLE_ENVIRONMENT_BEGIN_PROCENT
 */
public class EnvironmentVariableBeginSignMatcher extends AOnlyMapCharToToken {

    public EnvironmentVariableBeginSignMatcher() {
        super(RobotSingleCharTokenType.SINGLE_ENVIRONMENT_BEGIN_PROCENT);
    }
}
