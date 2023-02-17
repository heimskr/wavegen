package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class GameBoy(implicit clockFreq: Int) extends Module {
	val io = IO(new Bundle {
		val start   = Input(Bool())
		val rom     = Input(UInt(8.W))
		val sw      = Input(UInt(8.W))
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val out     = Output(UInt(7.W))
		val addr    = Output(UInt(18.W))
		val leds    = Output(UInt(8.W))
		val error   = Output(UInt(4.W))
	})

	val clocker = Module(new Clocker)
	clocker.io.enable := true.B
	clocker.io.freq   := 4_194_304.U

	val slow = Module(new Clocker)
	slow.io.enable := true.B
	slow.io.freq   := io.sw(7, 4) << 4.U

	val stateMachine = Module(new StateMachine)
	stateMachine.io.start := io.start
	stateMachine.io.tick  := clocker.io.tick
	stateMachine.io.rom   := io.rom
	io.addr := stateMachine.io.addr
	io.error := stateMachine.io.error

	io.leds := 0.U

	switch (io.sw(3, 0)) {
		is ( 0.U) { io.leds := stateMachine.io.errorInfo2(7, 0) }
		is ( 1.U) { io.leds := stateMachine.io.errorInfo2(15, 8) }
		is ( 2.U) { io.leds := stateMachine.io.errorInfo }
		is ( 3.U) { io.leds := Cat(0.U(4.W), stateMachine.io.state) }
		is ( 4.U) { io.leds := Cat(io.start, 0.U(3.W), stateMachine.io.error) }
		is ( 5.U) { io.leds := stateMachine.io.addr(17, 16) }
		is ( 6.U) { io.leds := stateMachine.io.addr( 7,  0) }
		is ( 7.U) { io.leds := stateMachine.io.addr(15,  8) }
		is ( 8.U) { io.leds := "b10101010".U }
		is ( 9.U) { io.leds := Fill(8, clocker.io.tick) }
		is (10.U) { io.leds := Fill(8,    slow.io.tick) }
		is (11.U) { io.leds := slow.io.period( 7,  0) }
		is (12.U) { io.leds := slow.io.period(15,  8) }
		is (13.U) { io.leds := slow.io.counter( 7,  0) }
		is (14.U) { io.leds := slow.io.counter(15,  8) }
		is (15.U) { io.leds := slow.io.counter(23, 16) }
	}

	val channel1 = Module(new Channel1)
	channel1.io.tick := clocker.io.tick
	channel1.io.registers := stateMachine.io.registers

	io.out := channel1.io.out
}
