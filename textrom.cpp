#include <array>
#include <cerrno>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <string>
#include <vector>

int main(int argc, char **argv) {
	constexpr size_t BUFFER_SIZE = 1024;

	std::freopen(nullptr, "rb", stdin);

	if (std::ferror(stdin))
		throw std::runtime_error(std::strerror(errno));

	std::size_t len;
	std::array<char, BUFFER_SIZE> buf;
	std::vector<char> input;

	while (0 < (len = std::fread(buf.data(), sizeof(buf[0]), buf.size(), stdin))) {
		if (std::ferror(stdin) && !std::feof(stdin))
			throw std::runtime_error(std::strerror(errno));
		input.insert(input.end(), buf.data(), buf.data() + len);
	}

	std::array<char, 80> arr;
	std::vector<decltype(arr)> lines;

	arr.fill(' ');
	size_t pos = 0;

	std::array<char, 80> empty = arr;

	auto pad = [&] {
		const size_t to_add = (45 - (lines.size() % 45)) % 45;
		for (size_t i = 0; i < to_add; ++i) {
			lines.push_back(empty);
		}
	};

	auto push = [&] {
		lines.push_back(arr);
		arr.fill(' ');
		pos = 0;
	};

	for (const char ch: input) {
		if (ch == '\n') {
			push();
		} else if (ch == '`') {
			push();
			pad();
		} else if (pos == 80) {
			throw std::runtime_error("Line too long");
		} else {
			arr[pos++] = ch;
		}
	}

	if (pos != 0)
		lines.push_back(arr);

	pad();

	for (const auto &line: lines)
		for (const char ch: line)
			std::cout << ch;
}
