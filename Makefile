ELSEWHERE ?= /home/kai/Downloads/hw/hw.srcs/sources_1/new

.PHONY: copy testDivider testGB testNES

all: .build

testDivider:
	sbt "testOnly *DividerTests"

testGB:
	sbt "testOnly *GameBoyTests"

testNES:
	sbt "testOnly *NESTests"

.build: $(shell find src/main -name '*.scala')
	sbt run && touch .build

copy: .build
ifeq ($(shell test -d "$(ELSEWHERE)" || echo fail),fail)
	$(error Please set the $$ELSEWHERE environment variable to a valid desired destination directory)
endif
	cp MainBoth.v    $(ELSEWHERE)/MainBoth.v && \
	cp Debouncer5.v  $(ELSEWHERE)/Debouncer5.v

slides:
	textrom < slides.txt | makecoe > slides.coe
