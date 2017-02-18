package com.elpassion.intelijidea.action.configure.configurator

import com.elpassion.intelijidea.common.LocalProperties
import com.elpassion.intelijidea.task.MFBeforeTaskDefaultSettingsProvider
import com.elpassion.intelijidea.task.MFTaskData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.reactivex.Maybe

fun mfConfigurator(project: Project) = { versionsList: List<String> ->
    mfConfiguratorImpl(project, { defaultValues -> showConfigurationDialog(project, versionsList, defaultValues) })
}

fun mfConfiguratorImpl(project: Project, configurationFromUi: (MFConfiguratorIn) -> Maybe<MFConfiguratorOut>): Maybe<String> {
    val provider = MFBeforeTaskDefaultSettingsProvider.INSTANCE
    val defaultValues = createDefaultValues(provider.taskData, project.getRemoteMachineName())
    return configurationFromUi(defaultValues)
            .doAfterSuccess { dataFromUi ->
                provider.saveConfiguration(createMFTaskData(project.basePath, dataFromUi))
                project.setRemoteMachineName(dataFromUi.remoteMachine)
            }
            .map { it.version }
}

private fun showConfigurationDialog(project: Project, versionsList: List<String>, defaultValues: MFConfiguratorIn) =
        Maybe.create<MFConfiguratorOut> { emitter ->
            MFConfiguratorDialog(project, versionsList, defaultValues, {
                emitter.onSuccess(it)
                emitter.onComplete()
            }, {
                emitter.onComplete()
            }).show()
        }

fun createDefaultValues(taskData: MFTaskData, remoteMachineName: String?): MFConfiguratorIn {
    return MFConfiguratorIn(remoteName = remoteMachineName,
            taskName = taskData.taskName,
            buildCommand = taskData.buildCommand)
}

fun createMFTaskData(basePath: String?, dataFromUi: MFConfiguratorOut): MFTaskData {
    return MFTaskData(mainframerPath = basePath,
            buildCommand = dataFromUi.buildCommand,
            taskName = dataFromUi.taskName)
}


private fun Project.getRemoteMachineName() = ApplicationManager.getApplication().runReadAction<String> {
    LocalProperties(basePath).readRemoteMachineName()
}

private fun Project.setRemoteMachineName(name: String) {
    ApplicationManager.getApplication().runWriteAction {
        LocalProperties(basePath).writeRemoteMachineName(name)
    }
}

private fun MFBeforeTaskDefaultSettingsProvider.saveConfiguration(dataFromUi: MFTaskData) {
    taskData = taskData.copy(
            buildCommand = dataFromUi.buildCommand,
            taskName = dataFromUi.taskName,
            mainframerPath = dataFromUi.mainframerPath)
}