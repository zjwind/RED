package org.robotframework.ide.core.testData.parser.util.lexer;

import java.util.LinkedList;
import java.util.List;


/**
 * Represents single matching status.
 * 
 * @author wypych
 * @serial RobotFramework 2.8.6
 * @serial 1.0
 * 
 */
public class MatchResult {

    private final MatchResult parent;
    private final IMatcher usedMatcher;
    private final MatchStatus status;
    private final Position matchPosition = new Position();
    private final List<MatchResult> subResults = new LinkedList<MatchResult>();
    private final List<String> messages = new LinkedList<String>();


    /**
     * @param usedMatcher
     * @param status
     */
    public MatchResult(final IMatcher usedMatcher, MatchStatus status) {
        this.usedMatcher = usedMatcher;
        this.status = status;
        this.parent = null;
    }


    /**
     * @param usedMatcher
     * @param status
     */
    private MatchResult(final MatchResult parent, final IMatcher usedMatcher,
            MatchStatus status) {
        this.usedMatcher = usedMatcher;
        this.status = status;
        this.parent = parent;
    }


    /**
     * 
     * @param subMatcher
     * @param subStatus
     */
    public void addSubResult(final IMatcher subMatcher, MatchStatus subStatus) {
        this.subResults.add(new MatchResult(this, subMatcher, subStatus));
    }


    /**
     * 
     * @param message
     *            an information about matching
     */
    public void addMessage(String message) {
        this.messages.add(message);
    }


    /**
     * 
     * @return all matching messages
     */
    public List<String> getMessages() {
        return this.messages;
    }


    /**
     * 
     * @return
     */
    public List<MatchResult> getSubResults() {
        return subResults;
    }


    /**
     * 
     * @return {@code null} or parent matcher in case this matching result was
     *         sub result
     */
    public MatchResult getParent() {
        return this.parent;
    }


    /**
     * 
     * @return
     */
    public IMatcher getMatcher() {
        return this.usedMatcher;
    }


    /**
     * 
     * @return
     */
    public Position getPosition() {
        return this.matchPosition;
    }


    /**
     * 
     * @return
     */
    public MatchStatus getStatus() {
        return this.status;
    }

    /**
     * Informs about status of matching performed
     * 
     * @author wypych
     * @serial RobotFramework 2.8.6
     * @serial 1.0
     * 
     */
    public static enum MatchStatus {
        NOT_FOUND, FOUND, IMMEDIATELLY_BREAK
    }
}
