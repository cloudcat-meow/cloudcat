package cloudstack.reporting

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.apis.Apis
import org.jclouds.cloudstack.compute.strategy.CloudStackComputeServiceAdapter
import org.jclouds.cloudstack.domain.VirtualMachine
import org.jclouds.compute.ComputeService
import org.jclouds.compute.ComputeServiceContext
import org.jclouds.compute.config.ComputeServiceProperties
import org.jclouds.compute.domain.ComputeMetadata
import org.jclouds.compute.domain.NodeMetadata
import org.jclouds.crypto.SshKeys
import org.jclouds.enterprise.config.EnterpriseConfigurationModule
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import org.jclouds.logging.jdk.config.JDKLoggingModule
import org.jclouds.providers.Providers
import org.jclouds.sshj.config.SshjSshClientModule

import com.google.common.base.Objects
import com.google.common.base.Predicate
import com.google.common.base.Strings
import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSortedSet
import com.google.common.collect.Iterables
import com.google.common.collect.ImmutableSet.Builder
import com.google.common.io.Closeables
import com.google.inject.Module

import cloudstack.reporting.Instance
import cloudstack.reporting.ReportRun

import org.apache.commons.logging.LogFactory


class ImportInstancesJob {
    private static final log = LogFactory.getLog(this)

    static triggers = {
        //cron name: 'importInstancesCron', cronExpression: "30 0/3 * * * ?"
        simple name: 'testTrigger', startDelay:1000, repeatInterval:600000, repeatCount:10
    }

    def group  = "importInstancesGroup"
    
    def execute() {
        def csAdapter = getCompute().getContext().utils().injector().getInstance(CloudStackComputeServiceAdapter.class)
        log.debug("Got csAdapter")
        def reportRun = new ReportRun()
        
        csAdapter.listNodes().each { vm ->
            def inst
            if (Instance.findByInstanceId(vm.id) != null) {
                inst = Instance.findByInstanceId(vm.id)
            } else {
                inst = new Instance()
            }

            inst.instanceId = vm.id
            inst.account = vm.account
            inst.name = vm.name
            inst.state = vm.state.toString()
            inst.cpuCount = vm.cpuCount
            inst.cpuUsed = vm.cpuUsed
            inst.created = vm.created
            inst.publicIPs = vm.nics.collect { n -> n.getIPAddress() }.join('|')
            inst.memory = vm.memory
            inst.templateId = vm.templateId
            inst.templateName = vm.templateName
            inst.serviceOfferingName = vm.serviceOfferingName
            inst.hostId = vm.hostId
            inst.rootDeviceId = vm.rootDeviceId
            if (inst.validate()) {
                reportRun.addToInstances(inst)
            } else {
                inst.errors.allErrors.each {
                    log.debug("inst ${inst.instanceId} error: ${it}")
                }
            }
        }
        reportRun.save()

        log.warn("finished")
    }

    def getModules() { 
        Iterable<Module> MODULES = new ImmutableSet.Builder<Module>().add(new SshjSshClientModule(),
                                                                          new SLF4JLoggingModule(),
                                                                          new EnterpriseConfigurationModule()).build();
        
        return MODULES
    }
    
    ComputeServiceContext ctx(String providerName, String identity, String credential, String endPointUrl) {
        Properties overrides = new Properties()
        if (!Strings.isNullOrEmpty(endPointUrl)) {
            overrides.setProperty(Constants.PROPERTY_ENDPOINT, endPointUrl)
        }
        return ctx(providerName, identity, credential, overrides)
    }
    
    ComputeServiceContext ctx(String providerName, String identity, String credential, Properties overrides) {
        // correct the classloader so that extensions can be found
        Thread.currentThread().setContextClassLoader(Apis.class.getClassLoader())
        return ContextBuilder.newBuilder(providerName)
        .credentials(identity, credential)
        .overrides(overrides)
        .modules(getModules())
        .buildView(ComputeServiceContext.class)
    }
    
    ComputeService getCompute() {
        def endPointUrl = "http://10.20.76.10:8080/client/api"
        def providerName = 'cloudstack'
        def identity = 'DxpY5x1Xh3c26BpOhPPAA8057_pRL2o_oBoqjUNBwlU0rl1NqOCDpXitQ0AdnjkzfJH6MJ0PvPw_cmlCUaIZIQ'
        def credential = 'IEntVYO8pkniQLurFSCBkzkfTsPaqBYl3buVd13A7y23ZF7MQtpDMnh61WN9eklnfG3MSHVob8fbpCWA/tJeKqbGuL9YBxBp68StxYajCZ5STlWah/q3Mu6ySPFyXD8l'
        def compute
        Properties overrides = new Properties()
        if (!Strings.isNullOrEmpty(endPointUrl)) {
            overrides.setProperty(Constants.PROPERTY_ENDPOINT, endPointUrl)
        }
        
        compute = ctx(providerName, identity, credential, overrides).getComputeService()
        
        return compute
    }

}
