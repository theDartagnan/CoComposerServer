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
package cocomposer.security.authentification;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.Md4PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 *
 * @author Remi Venant
 */
public enum AllowedPasswordEncoder {
    BC1(new BCryptPasswordEncoder(12)),
    TST(new Md4PasswordEncoder());

    private final PasswordEncoder encoder;

    private AllowedPasswordEncoder(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    public PasswordEncoder getEncoder() {
        return encoder;
    }

    public static AllowedPasswordEncoder getDefault() {
        return BC1;
    }
}
