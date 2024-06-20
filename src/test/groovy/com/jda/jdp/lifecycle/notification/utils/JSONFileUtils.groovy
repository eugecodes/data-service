/*
 * //==========================================================================
 * //               Copyright 2021, JDA Software Group, Inc.
 * //                            All Rights Reserved
 * //
 * //              THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF
 * //                         JDA SOFTWARE GROUP, INC.
 * //
 * //
 * //          The copyright notice above does not evidence any actual
 * //               or intended publication of such source code.
 * //
 * //==========================================================================
 */
package com.jda.jdp.lifecycle.notification.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

trait JSONFileUtils {
    def "readFromFile"(String fileName) {
        Path path = Paths.get(getClass().getClassLoader()
                .getResource(fileName).toURI())
        byte[] fileBytes = Files.readAllBytes(path)
        String data = new String(fileBytes)
        return data
    }

    def "readFromFileByteArray"(String fileName) {
        Path path = Paths.get(getClass().getClassLoader()
                .getResource(fileName).toURI())
        byte[] fileBytes = Files.readAllBytes(path)
        return fileBytes
    }
}
