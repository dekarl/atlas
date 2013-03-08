package org.atlasapi.equiv.results.scores;

import static org.junit.Assert.*;

import org.junit.Test;


public class ScoreThresholdTest {

    @Test
    public void testPresetThresholds() {
        assertTrue(ScoreThreshold.positive().apply(Score.ONE));
        assertFalse(ScoreThreshold.positive().apply(Score.ZERO));
        assertFalse(ScoreThreshold.positive().apply(Score.valueOf(-1d)));

        assertTrue(ScoreThreshold.nonNegative().apply(Score.ONE));
        assertTrue(ScoreThreshold.nonNegative().apply(Score.ZERO));
        assertFalse(ScoreThreshold.nonNegative().apply(Score.valueOf(-1d)));

        assertFalse(ScoreThreshold.negative().apply(Score.ONE));
        assertFalse(ScoreThreshold.negative().apply(Score.ZERO));
        assertTrue(ScoreThreshold.negative().apply(Score.valueOf(-1d)));

        assertFalse(ScoreThreshold.nonPositive().apply(Score.ONE));
        assertTrue(ScoreThreshold.nonPositive().apply(Score.ZERO));
        assertTrue(ScoreThreshold.nonPositive().apply(Score.valueOf(-1d)));
    }
    
    @Test
    public void testInvertingThresholds() {
        
        ScoreThreshold overHalf = ScoreThreshold.greaterThan(0.5);
        
        assertTrue(overHalf.apply(Score.ONE));
        assertFalse(overHalf.apply(Score.valueOf(0.5)));
        assertFalse(overHalf.apply(Score.ZERO));
        
        ScoreThreshold notOverHalf = ScoreThreshold.not(overHalf);
        
        assertFalse(notOverHalf.apply(Score.ONE));
        assertTrue(notOverHalf.apply(Score.valueOf(0.5)));
        assertTrue(notOverHalf.apply(Score.ZERO));
        
        ScoreThreshold notNotOverHalf = ScoreThreshold.not(notOverHalf);

        assertTrue(notNotOverHalf == overHalf);
        assertTrue(notNotOverHalf.apply(Score.ONE));
        assertFalse(notNotOverHalf.apply(Score.valueOf(0.5)));
        assertFalse(notNotOverHalf.apply(Score.ZERO));
        
    }

}
