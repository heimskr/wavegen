ELSEWHERE ?= /home/kai/Downloads/hw/hw.srcs/sources_1/new

test:
	sbt "testOnly *GameBoyTests"
	# sbt "testOnly *Channel1Tests"

copy:
	sbt run && cp MainGameBoy.v $(ELSEWHERE)/Main.v && cp Debouncer.v $(ELSEWHERE)/Debouncer.v
