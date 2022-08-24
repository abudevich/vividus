/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vividus.ssh;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableConsumer;
import org.jbehave.core.model.ExamplesTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.vividus.context.VariableContext;
import org.vividus.ssh.context.SshTestContext;
import org.vividus.ssh.exec.SshOutput;
import org.vividus.ssh.sftp.SftpCommand;
import org.vividus.ssh.sftp.SftpOutput;
import org.vividus.util.property.PropertyMappedCollection;
import org.vividus.variable.VariableScope;

@ExtendWith(MockitoExtension.class)
class SshStepsTests
{
    private static final String DESTINATION_PATH = "/path";
    private static final String SERVER = "my-server";
    private static final SshConnectionParameters SSH_CONNECTION_PARAMETERS = new SshConnectionParameters();

    @Mock private VariableContext variableContext;
    @Mock private Map<String, CommandExecutionManager<?>> commandExecutionManagers;
    @Mock private SshTestContext sshTestContext;

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    @Test
    void shouldConfigureDynamicConnection()
    {
        var connectionParametersTable = new ExamplesTable("|username |host       |port|password|\n"
                        + "|admin    |10.10.10.10|22  |Pa$$w0rd|");
        var steps = new SshSteps(null, variableContext, commandExecutionManagers, sshTestContext);
        var key = "new-connection";
        steps.configureSshConnection(key, connectionParametersTable);
        var sshConnectionParametersArgumentCaptor = ArgumentCaptor.forClass(SshConnectionParameters.class);
        verify(sshTestContext).addDynamicConnectionParameters(eq(key), sshConnectionParametersArgumentCaptor.capture());
        var parameters = sshConnectionParametersArgumentCaptor.getValue();
        assertAll(
                () -> assertEquals("admin", parameters.getUsername()),
                () -> assertEquals("10.10.10.10", parameters.getHost()),
                () -> assertEquals(22, parameters.getPort()),
                () -> assertEquals("Pa$$w0rd", parameters.getPassword())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "|any|\n|1|\n|2|" })
    void shouldFailToConfigureDynamicConnection(String tableAsString)
    {
        var connectionParametersTable = new ExamplesTable(tableAsString);
        var steps = new SshSteps(null, variableContext, commandExecutionManagers, sshTestContext);
        var key = "invalid-connection";
        var exception = assertThrows(IllegalArgumentException.class,
                () -> steps.configureSshConnection(key, connectionParametersTable));
        assertEquals("Exactly one row with SSH connection parameters is expected in ExamplesTable, but found "
                + connectionParametersTable.getRowCount(), exception.getMessage());
    }

    @Test
    void shouldExecuteSshCommands() throws CommandExecutionException
    {
        testSshCommandsExecution(Map.of(SERVER, SSH_CONNECTION_PARAMETERS), SERVER);
    }

    @Test
    void shouldThrowAnErrorOnMissingConnectionConfiguration()
    {
        var commands = mock(Commands.class);
        var steps = new SshSteps(new PropertyMappedCollection<>(Map.of()), variableContext, commandExecutionManagers,
                sshTestContext);
        var exception = assertThrows(IllegalArgumentException.class,
                () -> steps.executeCommands(commands, SERVER, Protocol.SSH));
        assertEquals("SSH connection with key 'my-server' is not configured in the current story nor in properties",
                exception.getMessage());
    }

    @Test
    void shouldExecuteSshCommandsUsingDynamicConnection() throws CommandExecutionException
    {
        var key = "dynamic";
        when(sshTestContext.getDynamicConnectionParameters(key)).thenReturn(Optional.of(SSH_CONNECTION_PARAMETERS));
        testSshCommandsExecution(Map.of(), key);
    }

    @Test
    void shouldExecuteSftpCommands() throws CommandExecutionException
    {
        testSftpExecution((steps, commands) -> steps.executeCommands(commands, SERVER, Protocol.SFTP));
    }

    @Test
    void shouldSaveSftpResult() throws CommandExecutionException
    {
        var scopes = Set.of(VariableScope.SCENARIO);
        var variableName = "sftp-result";
        var sftpOutput = testSftpExecution(
            (steps, commands) -> steps.saveSftpResult(commands, SERVER, scopes, variableName));
        verify(variableContext).putVariable(scopes, variableName, sftpOutput.getResult());
    }

    @Test
    void shouldCreateFileOverSftp() throws CommandExecutionException
    {
        var content = "content";
        testPutFile(SftpCommand.PUT, content, steps -> steps.createFileOverSftp(content, DESTINATION_PATH, SERVER));
    }

    @Test
    void testPutFileSftp() throws CommandExecutionException
    {
        var filePath = "/test.txt";
        testPutFile(SftpCommand.PUT_FROM_FILE, filePath,
            steps -> steps.copyFileOverSftp(filePath, DESTINATION_PATH, SERVER));
    }

    private void testSshCommandsExecution(Map<String, SshConnectionParameters> staticConnections, String connectionKey)
            throws CommandExecutionException
    {
        CommandExecutionManager<SshOutput> executionManager = mockGettingOfCommandExecutionManager(Protocol.SSH);
        var commands = new Commands("ssh-command");
        var output = new SshOutput();
        when(executionManager.run(SSH_CONNECTION_PARAMETERS, commands)).thenReturn(output);

        var steps = new SshSteps(new PropertyMappedCollection<>(staticConnections), variableContext,
                commandExecutionManagers, sshTestContext);
        var actual = steps.executeCommands(commands, connectionKey, Protocol.SSH);

        assertEquals(output, actual);
        verify(sshTestContext).putSshOutput(output);
    }

    private void testPutFile(SftpCommand command, String parameter,
            FailableConsumer<SshSteps, CommandExecutionException> stepExecutor) throws CommandExecutionException
    {
        CommandExecutionManager<SftpOutput> executionManager = mockGettingOfCommandExecutionManager(Protocol.SFTP);
        when(executionManager.run(eq(SSH_CONNECTION_PARAMETERS), argThat(commands -> {
            var singleCommands = commands.getSingleCommands(null);
            if (singleCommands.size() == 1)
            {
                var singleCommand = singleCommands.get(0);
                return singleCommand.getCommand() == command
                        && List.of(parameter, DESTINATION_PATH).equals(singleCommand.getParameters());
            }
            return false;
        }))).thenReturn(new SftpOutput());

        var sshSteps = new SshSteps(new PropertyMappedCollection<>(Map.of(SERVER, SSH_CONNECTION_PARAMETERS)),
                variableContext, commandExecutionManagers, sshTestContext);
        stepExecutor.accept(sshSteps);

        verify(sshTestContext).putSshOutput(null);
    }

    private SftpOutput testSftpExecution(
            FailableBiFunction<SshSteps, Commands, Object, CommandExecutionException> commandExecutor)
            throws CommandExecutionException
    {
        CommandExecutionManager<SftpOutput> executionManager = mockGettingOfCommandExecutionManager(Protocol.SFTP);
        var commands = new Commands("sftp-command");
        var output = new SftpOutput();
        output.setResult("sftp-output");
        when(executionManager.run(SSH_CONNECTION_PARAMETERS, commands)).thenReturn(output);

        var sshSteps = new SshSteps(new PropertyMappedCollection<>(Map.of(SERVER, SSH_CONNECTION_PARAMETERS)),
                variableContext, commandExecutionManagers, sshTestContext);
        var actual = commandExecutor.apply(sshSteps, commands);

        assertEquals(output, actual);
        verify(sshTestContext).putSshOutput(null);
        return output;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> CommandExecutionManager<T> mockGettingOfCommandExecutionManager(Protocol protocol)
    {
        var commandExecutionManager = mock(CommandExecutionManager.class);
        when(commandExecutionManagers.get(protocol.toString())).thenReturn(commandExecutionManager);
        return commandExecutionManager;
    }
}
