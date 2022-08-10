/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.jenkins.pluginhealth.scoring.probes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import io.jenkins.pluginhealth.scoring.model.Plugin;
import io.jenkins.pluginhealth.scoring.model.ProbeResult;
import io.jenkins.pluginhealth.scoring.service.PluginService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProbeEngineTest {

    @Mock
    private PluginService pluginService;

    @Test
    public void shouldBeAbleToRunSimpleProbe() {
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = mock(Probe.class);
        final ProbeEngine probeEngine = new ProbeEngine(List.of(probe), pluginService);
        final ProbeResult expectedResult = ProbeResult.success("foo", "bar");

        when(probe.doApply(plugin)).thenReturn(expectedResult);
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));
        probeEngine.run();

        verify(probe).doApply(plugin);
        verify(plugin).addDetails(expectedResult);
        verify(pluginService).saveOrUpdate(plugin);
    }

    @Test
    public void shouldNotAccessPluginWithPastResultAndReleaseRequirement() {
        final Plugin plugin = new Plugin("foo", "bar", ZonedDateTime.now().minusDays(1))
            .addDetails(ProbeResult.success("wiz", "This is good"));
        final Probe probe = mock(Probe.class);
        final ProbeEngine probeEngine = new ProbeEngine(List.of(probe), pluginService);

        when(probe.requiresRelease()).thenReturn(Boolean.TRUE);
        when(probe.key()).thenReturn("wiz");
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));
        probeEngine.run();

        verify(probe, never()).doApply(plugin);
    }

    @Test
    public void shouldNotSaveErrors() {
        final Plugin plugin = mock(Plugin.class);
        final Probe probe = mock(Probe.class);
        final ProbeEngine probeEngine = new ProbeEngine(List.of(probe), pluginService);

        when(probe.doApply(any(Plugin.class))).thenReturn(ProbeResult.error("foo", "bar"));
        when(pluginService.streamAll()).thenReturn(Stream.of(plugin));
        probeEngine.run();

        verify(plugin, never()).addDetails(any());
    }
}