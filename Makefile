test:
	sbt "testOnly *NoiseTests"

copy:
	sbt run && cp Main.v /home/kai/Downloads/hw/hw.srcs/sources_1/new/Main.v