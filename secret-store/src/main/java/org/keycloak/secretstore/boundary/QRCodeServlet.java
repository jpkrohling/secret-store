/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.secretstore.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.keycloak.secretstore.api.Token;
import org.keycloak.secretstore.api.TokenService;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

/**
 * @author Juraci Paixão Kröhling
 */
@PermitAll
@WebServlet(urlPatterns = "/qrcode")
public class QRCodeServlet extends HttpServlet {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    TokenService tokenService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String tokenIdAsString = req.getParameter("tokenId");
        String sizeAsString = req.getParameter("size");
        Principal principal = req.getUserPrincipal();

        int size = 250;

        if (null != sizeAsString && !sizeAsString.isEmpty()) {
            try {
                size = Integer.parseInt(req.getParameter("size"));
            } catch (Throwable t) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Size is invalid.");
                return;
            }
        }

        if (null == tokenIdAsString || tokenIdAsString.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Token key is missing.");
            return;
        }

        UUID tokenId;
        try {
            tokenId = UUID.fromString(tokenIdAsString);
        } catch (Throwable t) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Token key is invalid.");
            return;
        }

        Token token = tokenService.getByIdForDistribution(tokenId);
        if (null == token) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Token could not be found.");
            return;
        }

        if (!principal.getName().equals(token.getPrincipal())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Token could not be found for principal.");
            return;
        }

        String response = token.getId().toString() +
                "," +
                token.getSecret();

        if (null != token.getExpiresAt()) {
            response += "," + token.getExpiresAt().toString();
        }

        BitMatrix bitMatrix;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            bitMatrix = writer.encode(response, BarcodeFormat.QR_CODE, size, size);
        } catch (WriterException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while generating the QR Code.");
            return;
        }

        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOut);
        byte[] pngData = pngOut.toByteArray();

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("image/png");
        resp.getOutputStream().write(pngData);
    }
}
