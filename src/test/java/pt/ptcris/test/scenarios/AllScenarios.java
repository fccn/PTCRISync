package pt.ptcris.test.scenarios;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Run every scenarios defined in version 0.5 of the PTCRISync specification,
 * as well as additional ones dealing with technical issues.
 * 
 * @see Scenario
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    Scenario01.class,
    Scenario02.class,
    Scenario03.class,
    Scenario04.class,
    Scenario05.class,
    Scenario06.class,
    Scenario07.class,
    Scenario08.class,
    Scenario09.class,
    Scenario10.class,
    Scenario11.class,
    Scenario12.class,
    Scenario13.class,
    Scenario14.class,
    Scenario15.class,
    Scenario16.class,
    Scenario17.class,
    Scenario18.class,
    Scenario19.class,
    Scenario20.class,
    ScenarioInvalidLocal1.class,
    ScenarioInvalidLocal2.class,
    ScenarioPerformance.class
})

public class AllScenarios {}