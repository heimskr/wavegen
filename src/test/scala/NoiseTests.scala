package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chiseltest._
import chisel3.experimental.BundleLiterals._
import org.scalatest.flatspec.AnyFlatSpec
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class NoiseTests extends AnyFlatSpec with ChiselScalatestTester {
	behavior of "NoiseGen"
	it should "generate noise" in {
		val stream = new DataOutputStream(new FileOutputStream(new File("noise.raw")))

		test(new NoiseGen(65536)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
			dut.clock.setTimeout(0)
			for (i <- 0 until 100000) {
				dut.clock.step()
				stream.writeShort(dut.io.out.peek().litValue.toShort)
				// println(dut.io.out.peek().litValue.toShort)
			}
		}
	}
}
