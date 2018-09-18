/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.test.scenarios;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Run every scenarios defined in version 0.5 of the PTCRISync specification,
 * as well as additional ones dealing with technical issues.
 * 
 * @see ScenarioFunding
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
    ScenarioF01.class,
    ScenarioF02.class,
    ScenarioF03.class,
    ScenarioF04.class,
    ScenarioF05.class,
    ScenarioF06.class,
    ScenarioF07.class,
    ScenarioF08.class,
    ScenarioF09.class,
    ScenarioF10.class,
    ScenarioF11.class,
    ScenarioF12.class,
    ScenarioF13.class,
    ScenarioF14.class,
    ScenarioF15.class,
    ScenarioF16.class,
    ScenarioF17.class,
    ScenarioF18.class,
    ScenarioF19.class,
    ScenarioF20.class,
    ScenarioFIgnoredTypes.class,
    ScenarioFInvalidLocal.class,
//    ScenarioPerformance.class,
//    ScenarioPerformance2.class
})

public class AllScenarios {}