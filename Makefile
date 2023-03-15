ELSEWHERE ?= /home/kai/Downloads/hw/hw.srcs/sources_1/new

.PHONY: copy testDivider testGB testNES

all: .build

testDivider:
	sbt "testOnly *DividerTests"

testGB:
	sbt "testOnly *GameBoyTests"

testNES:
	sbt "testOnly *NESTests"

testScope:
	sbt "testOnly *OscilloscopeTests"

.build: $(shell find src/main -name '*.scala')
	sbt run && touch .build

copy: .build
ifeq ($(shell test -d "$(ELSEWHERE)" || echo fail),fail)
	$(error Please set the $$ELSEWHERE environment variable to a valid desired destination directory)
endif
	cp MainGB.v      $(ELSEWHERE)/MainGB.v      && \
	cp MainNES.v     $(ELSEWHERE)/MainNES.v     && \
	cp MainBoth.v    $(ELSEWHERE)/MainBoth.v    && \
	cp Debouncer3.v  $(ELSEWHERE)/Debouncer3.v  && \
	cp Debouncer5.v  $(ELSEWHERE)/Debouncer5.v  && \
	cp Debouncer8.v  $(ELSEWHERE)/Debouncer8.v  && \
	cp ImageOutput.v $(ELSEWHERE)/ImageOutput.v && \
	sed -i '1s/^/`default_nettype wire\n/' $(ELSEWHERE)/ImageOutput.v

slides:
	textrom < slides.txt | makecoe > slides.coe
