package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class Channel3(val instant: Boolean) extends Module {
	val io = IO(new ChannelIO {
		val lengthTick = Input(Bool())
		val channelOn  = Output(Bool())
	})

	// Pan Docs says it's bits 7–0, but in both worldmap.vgm and duel.vgm bits 7 and 6 are never set to 1, so presumably
	// it's only bits 5–0 that matter (just like in channels 1, 2 and 4).
	val lengthLoad     = io.registers.NR31(5, 0)
	val volume         = io.registers.NR32(6, 5)
	val wavelengthLow  = io.registers.NR33
	val wavelengthHigh = io.registers.NR34(2, 0)
	val lengthEnable   = io.registers.NR34(6)
	val trigger        = io.registers.NR34(7)
	val wavelength     = Cat(wavelengthHigh, wavelengthLow)

	val lengthCounter = Module(new LengthCounter)
	lengthCounter.io.tick      := io.lengthTick
	lengthCounter.io.trigger   := trigger
	lengthCounter.io.enable    := lengthEnable
	lengthCounter.io.loadValue := lengthLoad

	val periodClocker = Module(new PeriodClocker)
	periodClocker.io.tickIn       := io.tick
	periodClocker.io.period.valid := true.B
	periodClocker.io.period.bits  := (2048.U - wavelength) << 1.U

	io.out := 0.U

	val pointerReg   = RegInit(0.U(5.W))
	val storedSample = RegInit(0.U(4.W))

	val pointer = WireInit(pointerReg)
	val sample  = WireInit(storedSample)

	when (trigger) {
		pointerReg := 0.U
		pointer    := 0.U
	}

	when (lengthCounter.io.channelOn) {
		val sampleRegister = WireInit(0.U(8.W))

		when (periodClocker.io.tickOut) {
			when (pointerReg === 31.U) {
				pointerReg := 0.U
				if (instant)
					pointer := 0.U
			} .otherwise {
				pointerReg := pointerReg + 1.U
				if (instant)
					pointer := pointerReg + 1.U
			}

			switch (pointer >> 1.U) {
				is (0.U)  { sampleRegister := io.registers.WT0 }
				is (1.U)  { sampleRegister := io.registers.WT1 }
				is (2.U)  { sampleRegister := io.registers.WT2 }
				is (3.U)  { sampleRegister := io.registers.WT3 }
				is (4.U)  { sampleRegister := io.registers.WT4 }
				is (5.U)  { sampleRegister := io.registers.WT5 }
				is (6.U)  { sampleRegister := io.registers.WT6 }
				is (7.U)  { sampleRegister := io.registers.WT7 }
				is (8.U)  { sampleRegister := io.registers.WT8 }
				is (9.U)  { sampleRegister := io.registers.WT9 }
				is (10.U) { sampleRegister := io.registers.WTA }
				is (11.U) { sampleRegister := io.registers.WTB }
				is (12.U) { sampleRegister := io.registers.WTC }
				is (13.U) { sampleRegister := io.registers.WTD }
				is (14.U) { sampleRegister := io.registers.WTE }
				is (15.U) { sampleRegister := io.registers.WTF }
			}

			val shifted = Wire(UInt(4.W))

			when (pointer(0)) {
				shifted := sampleRegister(3, 0)
			} .otherwise {
				shifted := sampleRegister(7, 4)
			}

			storedSample := shifted
			sample := shifted
		}

		switch (volume) {
			is (0.U) { /* leave at 0 */ }
			is (1.U) { io.out := sample }
			is (2.U) { io.out := sample >> 1.U }
			is (3.U) { io.out := sample >> 2.U }
		}
	}

	io.channelOn := lengthCounter.io.channelOn
}
