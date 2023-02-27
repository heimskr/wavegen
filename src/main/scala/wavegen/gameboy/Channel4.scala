package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class Channel4 extends Module {
	val io = IO(new ChannelIO {
		val envelopeTick  = Input(Bool())
		val lengthTick    = Input(Bool())
		val channelOn     = Output(Bool())
		val currentVolume = Output(UInt(4.W))
	})

	val lengthLoad   = WireInit(io.registers.NR41(5, 0))
	val startVolume  = WireInit(io.registers.NR42(7, 4))
	val addMode      = WireInit(io.registers.NR42(3))
	val period       = WireInit(io.registers.NR42(2, 0))
	val clockShift   = WireInit(io.registers.NR43(7, 4))
	val lfsrWidth    = WireInit(io.registers.NR43(3))
	val clockDivider = WireInit(io.registers.NR43(2, 0))
	val trigger      = WireInit(io.registers.NR44(7))
	val lengthEnable = WireInit(io.registers.NR44(6))

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

	val periodClocker = Module(new PeriodClocker)
	periodClocker.io.tickIn := io.tick
	periodClocker.io.period.valid := true.B
	when (clockDivider === 0.U) {
		periodClocker.io.period.bits := 1.U << (clockShift + 3.U) // 16(0.5 * 2^s) = 8 * (2^s) = 2^(s+3) = 1 << (s+3)
	} .otherwise {
		periodClocker.io.period.bits := clockDivider << (clockShift + 4.U) // 16(r * 2^s) = r << s << 4 = r << (s + 4)
	}

	val lfsr = RegInit(1.U(15.W))

	io.out := 0.U

	when (lengthCounter.io.channelOn) {
		when (io.tick) {
			val bit = lfsr(1) ^ lfsr(0)
			when (lfsrWidth) {
				lfsr := Cat(bit, lfsr(14, 8), bit, lfsr(6, 1))
			} .otherwise {
				lfsr := Cat(bit, lfsr(14, 1))
			}
		}

		io.out := Mux(lfsr(0), 0.U, envelope.io.currentVolume >> 1.U)
	}

	io.channelOn     := lengthCounter.io.channelOn
	io.currentVolume := envelope.io.currentVolume
}
