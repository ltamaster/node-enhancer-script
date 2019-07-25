package com.rundeck.plugins

import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.DefaultScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.descriptions.TextArea
import com.dtolabs.rundeck.plugins.nodes.IModifiableNodeEntry
import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin


@Plugin(service = ServiceNameConstants.NodeEnhancer, name = ScriptNodeEnhancer.PROVIDER)
@PluginDescription(title = "Add attribute from Script", description = "")
class ScriptNodeEnhancer implements NodeEnhancerPlugin {

    static final String PROVIDER = "node-enhancer-script"

    @PluginProperty(
            title = "Script",
            description = "Script to execute that will get the new attribute value",
            required = true
    )
    @RenderingOptions(
            [
                    @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = 'CODE'),
                    @RenderingOption(key = 'codeSyntaxMode', value = 'sh'),
                    @RenderingOption(key = 'codeSyntaxSelectable', value = 'true'),
            ]
    )
    String script


    @PluginProperty(
            title = "File Extension",
            description = "File extension for the script",
            defaultValue = "sh"
    )
    String scriptFileExt
    @PluginProperty(
            title = "Arguments",
            description = "Arguments for the script",
            required = false
    )
    String scriptArgs

    @PluginProperty(
            title = "Interpreter",
            description = "Interpreter for the script",
            required = false
    )
    String scriptInterpreter


    @PluginProperty(
            title = "attribute",
            description = "Attribute name that will be added to the node",
            required = false
    )
    String attribute

    private ScriptFileNodeStepUtils scriptfileUtils = new DefaultScriptFileNodeStepUtils()


    @Override
    void updateNode(String project, IModifiableNodeEntry node) {

        try{
            File file = createTempScriptFile(script)
            def commandArray = [scriptInterpreter, file.absolutePath]

            if(scriptArgs){
                commandArray.add(scriptArgs)
            }
            println("[updateNode]  running command ${commandArray}")

            ProcessBuilder processBuilder = new ProcessBuilder()
            processBuilder.command(commandArray)

            //adding node attributes as envs
            node.getAttributes().each {key,value->
                processBuilder.environment().put("RD_NODE_${key.toUpperCase()}", value)
            }
            Process process = processBuilder.start()

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line
            StringBuffer result = new StringBuffer()
            while ((line = reader.readLine()) != null) {
                result.append(line)
                println("[updateNode] ${line}")
            }

            int exitValue = process.waitFor()

            if (exitValue == 0) {
                //if script finished OK it will add a new attribute with the scipt's output
                node.addAttribute(attribute, result.toString())
            } else {
                println("[updateNode] script finished with exit code ${exitValue}")
            }
            file.delete()
        }catch(Exception e){
            e.printStackTrace()
        }
    }

    File createTempScriptFile(String command) {
        File file = File.createTempFile("temp", "." + scriptFileExt)
        file.setExecutable(true)
        file.write command

        return file
    }
}
