ELSEWHERE ?= /home/kai/Downloads/hw/hw.srcs/sources_1/new

all: copy

test:
	sbt "testOnly *GameBoyTests"
	# sbt "testOnly *Channel1Tests"
	# sbt "testOnly *DividerTests"

copy:
	sbt run \
		&& cp MainGameBoy.v $(ELSEWHERE)/Main.v        \
		&& cp Debouncer.v   $(ELSEWHERE)/Debouncer.v   \
		&& cp ImageOutput.v $(ELSEWHERE)/ImageOutput.v \
		&& sed -i '1s/^/`default_nettype wire\n/' $(ELSEWHERE)/ImageOutput.v
