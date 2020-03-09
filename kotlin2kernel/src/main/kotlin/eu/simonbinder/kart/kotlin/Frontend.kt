package eu.simonbinder.kart.kotlin

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import java.io.File

/**
 * Compile Kotlin sources and libraries into IR.
 */
class Frontend(
    private val messageCollector: MessageCollector
) {

    val config = CompilerConfiguration()
    private val sourceRoots = mutableListOf<File>()

    var dartBackendContext: DartBackendContext? = null

    fun addSourceRoot(file: File) {
        sourceRoots.add(file)
    }

    fun obtainIr(lower: Boolean = true): IrModuleFragment {
        val environment = KotlinCoreEnvironment.createForProduction(Disposable {  }, config, EnvironmentConfigFiles.JVM_CONFIG_FILES).also {
            it.addKotlinSourceRoots(sourceRoots)
        }

        val resolved = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            environment.project,
            environment.getSourceFiles(),
            CliBindingTrace(),
            config,
            environment::createPackagePartProvider
        )
        val languageVersionSettings = config.languageVersionSettings

        val irGeneratorContext = GeneratorContext(
            Psi2IrConfiguration(),
            resolved.moduleDescriptor,
            resolved.bindingContext,
            languageVersionSettings,
            SymbolTable(),
            GeneratorExtensions()
        )

        val fragment = Psi2IrTranslator(languageVersionSettings, irGeneratorContext.configuration)
            .generateModuleFragment(irGeneratorContext, environment.getSourceFiles())

        if (lower) {
            val phaseConfig = createPhaseConfig(dartPhases, DartCompilerArguments(), messageCollector)
            dartBackendContext = DartBackendContext(
                resolved.moduleDescriptor, irGeneratorContext.irBuiltIns, irGeneratorContext.symbolTable, fragment, config)

            dartPhases.invokeToplevel(phaseConfig, dartBackendContext!!, fragment)
        }

        return fragment
    }
}