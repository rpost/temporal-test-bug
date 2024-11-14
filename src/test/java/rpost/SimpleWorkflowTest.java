package rpost;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.management.Query;

public class SimpleWorkflowTest {

    static final String TASK_QUEUE = "test-queue";

    TestWorkflowEnvironment testEnv;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    void shouldSignalWithStart() {

        Worker worker = testEnv.newWorker(TASK_QUEUE);
        WorkflowClient client = testEnv.getWorkflowClient();
        worker.registerWorkflowImplementationTypes(
                SampleWorkflow.SampleWorkflowImpl.class,
                SampleWorkflow.MutexWorkflowImpl.class
        );

        worker.registerActivitiesImplementations(
                new SampleWorkflow.SignalWithStartActivityImpl(client)
        );

        testEnv.start();

        SampleWorkflow sampleWorkflow1 = client.newWorkflowStub(
                SampleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId("my-workflow-1")
                        .build()
        );

        SampleWorkflow sampleWorkflow2 = client.newWorkflowStub(
                SampleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId("my-workflow-2")
                        .build()
        );

        WorkflowStub workflowStub1 = WorkflowStub.fromTyped(sampleWorkflow1);
        WorkflowStub workflowStub2 = WorkflowStub.fromTyped(sampleWorkflow2);
        WorkflowClient.start(sampleWorkflow1::execute, "mutexId");
        WorkflowClient.start(sampleWorkflow2::execute, "mutexId");

        workflowStub1.getResult(Void.class);
        workflowStub2.getResult(Void.class);

        WorkflowStub mutexWorkflow = WorkflowStub.fromTyped(client.newWorkflowStub(
                SampleWorkflow.MutexWorkflow.class,
                "mutexId"
        ));

        mutexWorkflow.getResult(Void.class);
    }

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker(TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(
                SampleWorkflow.SampleWorkflowImpl.class,
                SampleWorkflow.MutexWorkflowImpl.class
        );

        worker.registerActivitiesImplementations(
                new SampleWorkflow.SignalWithStartActivityImpl(client)
        );

        factory.start();

        SampleWorkflow sampleWorkflow3 = client.newWorkflowStub(
                SampleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId("my-workflow-1")
                        .build()
        );

        SampleWorkflow sampleWorkflow2 = client.newWorkflowStub(
                SampleWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId("my-workflow-2")
                        .build()
        );

        WorkflowStub workflowStub1 = WorkflowStub.fromTyped(sampleWorkflow3);
        WorkflowStub workflowStub2 = WorkflowStub.fromTyped(sampleWorkflow2);
        WorkflowClient.start(sampleWorkflow3::execute, "mutexId");
        WorkflowClient.start(sampleWorkflow2::execute, "mutexId");

        workflowStub1.getResult(Void.class);
        workflowStub2.getResult(Void.class);

        WorkflowStub mutexWorkflow = WorkflowStub.fromTyped(client.newWorkflowStub(
                SampleWorkflow.MutexWorkflow.class,
                "mutexId"
        ));

        mutexWorkflow.getResult(Void.class);

        factory.shutdownNow();
    }
}
