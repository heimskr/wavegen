#include <array>
#include <cerrno>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <optional>
#include <string>
#include <vector>

int main(int argc, char **argv) {
	constexpr size_t BUFFER_SIZE = 1024;

	constexpr size_t WIDTH  = 40;
	constexpr size_t HEIGHT = 22;

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

	std::array<char, WIDTH> arr;
	std::vector<decltype(arr)> lines;

	arr.fill(' ');
	size_t pos = 0;

	std::array<char, WIDTH> empty = arr;

	auto pad = [&] {
		const size_t to_add = (HEIGHT - (lines.size() % HEIGHT)) % HEIGHT;
		for (size_t i = 0; i < to_add; ++i)
			lines.push_back(empty);
	};

	auto push = [&] {
		lines.push_back(arr);
		arr.fill(' ');
		pos = 0;
	};

	size_t timeout = 0;

	for (size_t i = 0; i < input.size(); ++i) {
		const char ch = input[i];
		std::optional<char> to_add;
		if (ch == '\\') {
			if (i != input.size() - 1) {
				const char next = input[i + 1];
				if (next == 's') {
					push();
					pad();
					timeout = 2;
				} else if (next == '\\') {
					to_add = '\\';
				} else
					throw std::runtime_error("Invalid escape: " + std::string{ch, next});
			} else
				to_add = '\\';
		} else if (0 < timeout) {
			--timeout;
		} else if (ch == '\n') {
			push();
		} else {
			to_add = ch;
		}

		if (to_add) {
			if (pos == WIDTH)
				throw std::runtime_error("Line too long");
			arr[pos++] = *to_add;
		}
	}

	if (pos != 0)
		lines.push_back(arr);

	pad();

	for (const auto &line: lines)
		for (const char ch: line)
			std::cout << ch;
}
