package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.{Files, Paths}
import scala.util.control.Breaks._

class Channel1TestModule(implicit clockFreq: Int) extends Module {
	val io = IO(new Bundle {
		val out = Output(UInt(4.W))
		val NR10 = Input(UInt(8.W))
		val NR11 = Input(UInt(8.W))
		val NR12 = Input(UInt(8.W))
		val NR13 = Input(UInt(8.W))
		val NR14 = Input(UInt(8.W))
	})

	val registers = RegInit(0.U.asTypeOf(Registers()))
	registers.NR10 := io.NR10
	registers.NR11 := io.NR11
	registers.NR12 := io.NR12
	registers.NR13 := io.NR13
	registers.NR14 := io.NR14

	val clocker = Module(new Clocker)
	clocker.io.enable := true.B
	clocker.io.freq := 1_000_000.U
	
	val channel1 = Module(new Channel1(500_000))
	channel1.io.tick := clocker.io.tick
	channel1.io.registers := registers
	io.out := channel1.io.out
}

class Channel1Tests extends AnyFlatSpec with ChiselScalatestTester {
	implicit val clockFreq = 10_000_000

	behavior of "Channel1"
	it should "do something" in {
		test(new Channel1TestModule).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.clock.setTimeout(0)
			dut.io.NR10.poke(0)
			dut.io.NR11.poke(0)
			dut.io.NR12.poke(0)
			dut.io.NR13.poke(0)
			dut.io.NR14.poke(0)
			dut.clock.step(10)
			dut.io.NR12.poke("b00001000".U)
			dut.io.NR14.poke("b10000000".U)
			dut.clock.step(20)
			dut.io.NR12.poke("b00001000".U)
			dut.clock.step(10)
			dut.io.NR13.poke("b00000000".U)
			dut.io.NR11.poke("b10000000".U)
			// dut.io.NR14.poke("b00000000".U)
			dut.io.NR13.poke("b00000000".U)
			dut.io.NR14.poke("b10000111".U) // Trigger = 1, high bits of input freq = 111

			dut.clock.step(50)
			dut.io.NR14.poke("b00000000".U)

			for (i <- 0 to 100) {
				dut.clock.step(1000)
				println(i * 1000)
			}
		}
	}
}
