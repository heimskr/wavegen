package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class Channel2 extends Module {
	val io = IO(new ChannelIO {
		val envelopeTick = Input(Bool())
		val lengthTick   = Input(Bool())
		val buttonD      = Input(Bool())
		val buttonR      = Input(Bool())
		val freq         = Output(UInt(11.W))
	})

	val duty         = io.registers.NR21(7, 6)
	val lengthLoad   = io.registers.NR21(5, 0)
	val startVolume  = io.registers.NR22(7, 4)
	val addMode      = io.registers.NR22(3)
	val period       = io.registers.NR22(2, 0)
	val trigger      = io.registers.NR24(7)
	val lengthEnable = io.registers.NR24(6)
	val frequency    = Cat(io.registers.NR24(2, 0), io.registers.NR23)

	val envelope = Module(new Envelope)
	envelope.io.tick          := io.envelopeTick
	envelope.io.trigger       := trigger
	envelope.io.initialVolume := startVolume
	envelope.io.rising        := addMode
	envelope.io.period        := period

	val lengthCounter = Module(new LengthCounter)
	lengthCounter.io.tick      := io.lengthTick
	lengthCounter.io.trigger   := trigger
	lengthCounter.io.enable    := lengthEnable
	lengthCounter.io.loadValue := lengthLoad

	val waveforms = VecInit(Seq("b00000001".U, "b00000011".U, "b00001111".U, "b11111100".U))

	val periodClocker = Module(new PeriodClocker)
	periodClocker.io.tickIn       := io.tick
	periodClocker.io.period.bits  := (2048.U - frequency) << 2.U
	periodClocker.io.period.valid := true.B

	val squareGen = Module(new SquareGenExternal(1, 8))
	squareGen.io.tick := periodClocker.io.tickOut
	squareGen.io.max  := "b1".U
	squareGen.io.wave := waveforms(duty)

	io.out  := 0.U
	io.freq := frequency

	when (lengthCounter.io.channelOn || io.buttonD) {
		when (io.buttonR) {
			io.out := Mux(squareGen.io.out(0), "b1111".U, "b0000".U)
		} .otherwise {
			io.out := Mux(squareGen.io.out(0), envelope.io.currentVolume, 0.U)
		}
	}
}
