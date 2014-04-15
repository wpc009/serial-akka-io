all:
	sbt compile package
	scp ./target/scala-2.10/serial-akka-io_2.10-0.1.2.jar debian@192.168.0.33:~/serial-bridge-0.1.0/lib