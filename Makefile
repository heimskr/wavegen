ELSEWHERE ?= /home/kai/Downloads/hw/hw.srcs/sources_1/new

.PHONY: copy

all: .build

test:
	# sbt "testOnly *GameBoyTests"
	sbt "testOnly *NESTests"
	# sbt "testOnly *Channel1Tests"
	# sbt "testOnly *DividerTests"

.build: $(shell find src/main -name '*.scala')
	sbt run && touch .build

copy: .build
	cp MainGB.v      $(ELSEWHERE)/MainGB.v      \
	cp MainNES.v     $(ELSEWHERE)/MainNES.v     \
	cp Debouncer2.v  $(ELSEWHERE)/Debouncer2.v  \
	cp Debouncer5.v  $(ELSEWHERE)/Debouncer5.v  \
	cp ImageOutput.v $(ELSEWHERE)/ImageOutput.v \
	sed -i '1s/^/`default_nettype wire\n/' $(ELSEWHERE)/ImageOutput.v

slides:
	textrom < slides.txt | makecoe > slides.coe
