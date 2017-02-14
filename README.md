# SCOIR Technical Interview for Back-End Engineers

## Additional/clarification requirements via emails with Steve
1. Once a file has been processed, the system deletes it from the input-directory.
1. Files will be considered new if the file name has not been recorded as processed before.
1. In the event of file name collision, the latest file should overwrite the earlier version.
1. The system should continue in the event of an invalid row; all errors should be collected and added to the corresponding error csv
1. There should be one error file created per input file (if errors exist)
1. The error file should match the name of the input file

## Assumptions
1. csv files will have headers
1. csv files dropped into the input directory will be valid csv files
1. The set of 'files already processed' will be cleared when the program ends. NOTE: This is a very slow memory leak, because this set is never cleared while the program is running.
1. Expecting to receive csv files of any size, from empty to as big as the hard drive will allow.
1. Assuming that 8 hours is the hard limit, it's better to have a program that works with fewer automated tests, rather than a program that does not work and has 100% automated coverage.
   I know this can be a 'sensitive' subject, so I wanted to be clear why I went the route I did.

## How-To Run the jar
You'll only need Java 7+ if you want to run the executable jar.

1. Download the standalone jar from the target folder.
1. Open a command prompt and navigate to the jar.
1. Run: java -jar scoir-0.1.0-SNAPSHOT-standalone.jar "FULL_PATH_TO_INPUT_DIRECTORY" "FULL_PATH_TO_OUTPUT_DIRECTORY" "FULL_PATH_TO_ERROR_DIRECTORY"

    ex. java -jar scoir-0.1.0-SNAPSHOT-standalone.jar "C:\Users\C\input" "C:\Users\C\output" "C:\Users\C\error"

## How-To compile and run
1. You'll need at least Java 7+ installed.
1. Download the Clojure build tool, Lein, from https://leiningen.org/#install
1. Open a command prompt and navigate to the root of the project.
1. Type in 'lein run', and it will compile and run the program.

