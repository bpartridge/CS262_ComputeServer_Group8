#!/bin/bash

(cd bin && rmiregistry &)
(cd bin && java -Djava.security.policy=../stupid.policy group8.Worker &)
(cd bin && java -Djava.security.policy=../stupid.policy group8.TestServerForWorker &)

# wait
# trap "kill 0" SIGINT SIGTERM EXIT
