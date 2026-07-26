package com.backend.sensor_data.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.backend.sensor_data.util.JwtUtil;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Covers JwtAuthFilter.doFilter branch-by-branch:
 * 1. non-sensor endpoint, any method -> always passes through
 * 2. sensor endpoint + POST (ingest) -> passes through, no auth check
 * 3. sensor endpoint + GET, no Authorization -> 401
 * 4. sensor endpoint + GET, malformed header -> 401
 * 5. sensor endpoint + GET, valid Bearer token -> passes through
 * 6. sensor endpoint + GET, invalid/expired token -> 401
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthFilter filter;

    private StringWriter responseBody;

    @BeforeEach
    void setUp() {
        responseBody = new StringWriter();
        // Writer is stubbed lazily per-test via stubWriter(), only where the filter
        // actually writes a body.
    }

    private void stubWriter() throws IOException {
        PrintWriter writer = new PrintWriter(responseBody);
        when(response.getWriter()).thenReturn(writer);
    }

    // ---- 1. Non-sensor endpoint: always bypasses auth, regardless of method ----

    @Test
    void nonSensorEndpoint_GET_bypassesAuthEntirely() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/me");
        // request.getMethod() is deliberately left unstubbed: for a non-sensor
        // URI the filter never reaches the method check, so stubbing it here
        // would be unnecessary and fail Mockito's strict-stubbing rule.

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtUtil, never()).validateAndGetClaims(any());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void nonSensorEndpoint_POST_bypassesAuthEntirely() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/signup");
        // Same reasoning as above: the method check is never reached for a non-sensor
        // URI.

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtUtil, never()).validateAndGetClaims(any());
    }

    // ---- 2. Sensor endpoint + POST (ingestion): bypasses auth ----

    @Test
    void sensorEndpoint_POST_traffic_bypassesAuth() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/sensors/traffic");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtUtil, never()).validateAndGetClaims(any());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void sensorEndpoint_POST_air_bypassesAuth() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/sensors/air");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void sensorEndpoint_POST_light_bypassesAuth() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/sensors/light");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void sensorEndpoint_postMethodLowercase_stillBypassesAuth() throws Exception {
        // Confirms the equalsIgnoreCase branch, not just exact "POST"
        when(request.getRequestURI()).thenReturn("/api/sensors/traffic");
        when(request.getMethod()).thenReturn("post");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(jwtUtil, never()).validateAndGetClaims(any());
    }

    // ---- 3. Sensor endpoint + GET, no Authorization header ----

    @Test
    void sensorEndpoint_GET_noAuthHeader_returns401() throws Exception {
        stubWriter();
        when(request.getRequestURI()).thenReturn("/api/sensors/traffic");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(chain, never()).doFilter(any(), any());
        assertThat(responseBody.toString()).contains("Not authenticated");
    }

    // ---- 4. Sensor endpoint + GET, malformed header (no "Bearer " prefix) ----

    @Test
    void sensorEndpoint_GET_malformedHeader_returns401() throws Exception {
        stubWriter();
        when(request.getRequestURI()).thenReturn("/api/sensors/light");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
        verify(jwtUtil, never()).validateAndGetClaims(any());
    }

    @Test
    void sensorEndpoint_GET_emptyBearerToken_stillReachesValidation() throws Exception {
        // "Bearer " with nothing after it -- header check passes (startsWith true),
        // substring(7) yields "", and validation is delegated to JwtUtil.
        stubWriter();
        when(request.getRequestURI()).thenReturn("/api/sensors/traffic");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        doThrow(new JwtException("empty token")).when(jwtUtil).validateAndGetClaims("");

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
    }

    // ---- 5. Sensor endpoint + GET, valid token ----

    @Test
    void sensorEndpoint_GET_validToken_passesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/sensors/traffic/stats");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.value");
        when(jwtUtil.validateAndGetClaims("valid.token.value")).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    // ---- 6. Sensor endpoint + GET, invalid/expired token ----

    @Test
    void sensorEndpoint_GET_invalidToken_returns401() throws Exception {
        stubWriter();
        when(request.getRequestURI()).thenReturn("/api/sensors/air/stats");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token");
        doThrow(new JwtException("expired")).when(jwtUtil).validateAndGetClaims("expired.token");

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(chain, never()).doFilter(any(), any());
        assertThat(responseBody.toString()).contains("Invalid or expired token");
    }

    @Test
    void sensorEndpoint_uriPrefixMatch_notExactSegment_isStillTreatedAsSensorEndpoint() throws Exception {
        // Guards against a future regression where someone tightens the prefix check;
        // documents the CURRENT behavior of startsWith("/api/sensors").
        when(request.getRequestURI()).thenReturn("/api/sensorsXYZ/whatever");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn(null);
        stubWriter();

        filter.doFilter(request, response, chain);

        // Under current implementation this IS treated as a sensor endpoint (prefix
        // match),
        // so it should 401 without a valid token. If this ever changes intentionally,
        // update this test alongside the fix.
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
