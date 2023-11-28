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
package cocomposer.config;

import cocomposer.model.CompositionRepository;
import cocomposer.model.MemberCredentialRepository;
import cocomposer.model.MemberRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 *
 * @author Remi Venant
 */
@TestConfiguration
public class TestDatasetConfig {

    @Bean
    public TestDatasetGenerator testDatasetGenerator(MemberRepository memberRepo,
            MemberCredentialRepository memberCredRepo, PasswordEncoder passwordEncoder,
            CompositionRepository compoRepo) {
        return new TestDatasetGenerator(memberRepo, memberCredRepo, passwordEncoder, compoRepo);
    }
}
