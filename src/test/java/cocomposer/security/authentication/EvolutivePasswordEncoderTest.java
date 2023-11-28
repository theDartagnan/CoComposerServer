/*
 * Copyright (C) 2023 IUT Laval - Le Mans Université.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cocomposer.security.authentication;

import cocomposer.security.authentification.AllowedPasswordEncoder;
import cocomposer.security.authentification.EvolutivePasswordEncoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 *
 * @author Remi Venant
 */
public class EvolutivePasswordEncoderTest {

    private static final Log LOG = LogFactory.getLog(EvolutivePasswordEncoderTest.class);

    private EvolutivePasswordEncoder testedEncoder;

    public EvolutivePasswordEncoderTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        this.testedEncoder = new EvolutivePasswordEncoder();
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testEncodeAndMatchWithDefault() {
        String rawPwd = "MySuperPass";

        AllowedPasswordEncoder dfltEncoder = AllowedPasswordEncoder.getDefault();
        String dfltEncoderKey = dfltEncoder.name();

        String encodedPwd = this.testedEncoder.encode(rawPwd);
        LOG.info("For password >" + rawPwd + "< encoded pass with default encoder \"" + dfltEncoderKey + "\": >" + encodedPwd + "<");

        assertThat(encodedPwd).as("Encode with default encoded pwd starts with encoder key").startsWith(dfltEncoderKey);

        boolean matchResult = this.testedEncoder.matches(rawPwd, encodedPwd);
        assertThat(matchResult).as("Match on default encoded pwd ok").isTrue();
    }

    @Test
    public void testEncodeWithAllowedAlgorithm() {
        String rawPwd = "MySuperPass";

        AllowedPasswordEncoder tstEncoder = AllowedPasswordEncoder.TST;
        String dfltEncoderKey = tstEncoder.name();

        String encodedPwd = this.testedEncoder.encodeWithAllowedAlgorithm(rawPwd, tstEncoder);
        LOG.info("For password >" + rawPwd + "< encoded pass with encoder \"" + dfltEncoderKey + "\": >" + encodedPwd + "<");

        assertThat(encodedPwd).as("Encode with default encoded pwd starts with encoder key").startsWith(dfltEncoderKey);
        boolean matchResult = this.testedEncoder.matches(rawPwd, encodedPwd);
        assertThat(matchResult).as("Match on default encoded pwd ok").isTrue();
    }

}
