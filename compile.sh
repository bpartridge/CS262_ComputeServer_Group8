#!/bin/bash

(cd src && javac -g -d ../bin edu/harvard/cs262/ComputeServer/*.java group8/*.java)
(cd bin && rmic group8.TestServerForWorker group8.QueuedServer group8.Worker)
