package wavegen.nes

import chisel3._
import chisel3.util._

class FrameCounter extends Module {
	val io = IO(new Bundle {
		val cpuTick  = Input(Bool())
		val register = Input(UInt(8.W)) // $4017
		val reload   = Input(Bool())
		val ticks    = Output(Ticks())
	})

	val toggle = RegInit(true.B)
	io.ticks.apu     := toggle
	io.ticks.quarter := false.B
	io.ticks.half    := false.B

	val counter       = RegInit(0.U(16.W))
	val fiveStep      = io.register(7)
	// Ignore IRQ inhibit :)
	val reloadCounter = RegInit(0.U(3.W))
	val midAPU        = RegInit(false.B)

	when (io.cpuTick) {
		toggle := !toggle

		when (io.reload) {
			reloadCounter := 1.U
			midAPU        := counter(0)
		} .elsewhen ((reloadCounter === 3.U && midAPU) || (reloadCounter === 4.U && !midAPU)) {
			reloadCounter := 0.U
			counter := 0.U
		} .elsewhen (reloadCounter =/= 0.U) {
			reloadCounter := reloadCounter + 1.U
		}

		when (fiveStep) {
			switch (counter) {
				is (7457.U) {
					io.ticks.quarter := true.B
				}

				is (14913.U) {
					io.ticks.quarter := true.B
					io.ticks.half    := true.B
				}

				is (22371.U) {
					io.ticks.quarter := true.B
				}

				is (37281.U) {
					io.ticks.quarter := true.B
					io.ticks.half    := true.B
					counter          := 0.U
				}
			}
		} .otherwise {
			switch (counter) {
				is (7457.U) {
					io.ticks.quarter := true.B
				}

				is (14913.U) {
					io.ticks.quarter := true.B
					io.ticks.half    := true.B
				}

				is (22371.U) {
					io.ticks.quarter := true.B
				}

				is (29829.U) {
					io.ticks.quarter := true.B
					io.ticks.half    := true.B
					counter          := 0.U
				}
			}
		}
	}
}
