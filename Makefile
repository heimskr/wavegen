test:
	sbt "testOnly *StateMachineTests"

copy:
	sbt run && cp MainGameBoy.v /home/kai/Downloads/hw/hw.srcs/sources_1/new/Main.v
