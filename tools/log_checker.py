import os


log_folder = os.path.join('..', 'logs')


def check_errors():
	for file in os.listdir(log_folder):
		if file.endswith('.stderr'):
			with open(os.path.join(log_folder, file)) as f:
				content = f.read()
				if len(content) > 0:
					print(f'Error in {file}:\n {content}')


def get_messages():
	messages = dict()
	for file in os.listdir(log_folder):
		if file.endswith('.output'):
			process_name = file.split('.')[0]
			with open(os.path.join(log_folder, file)) as f:
				messages[process_name] = f.read().splitlines()
	return messages


def check_no_duplication(proc_messages):
	if not all(len(messages) == len(set(messages)) for messages in proc_messages.values()):
		print("Violated no duplication")
		return False
	return True


def check_no_creation(proc_messages, total_number_messages):
	for proc, messages in proc_messages.items():
		for message in messages:
			if message.startswith('d'):
				content = message.split(' ')[2]
				if not (1 <= int(content) <= total_number_messages):
					print(f"Violated no creation on process {proc}")
					return False
	return True


def check_uniform_agreement(proc_messages):
	for proc, messages in proc_messages.items():
		for message in messages:
			if not message.startswith("d"):
				continue
			for prc, msgs in proc_messages.items():
				if message not in msgs:
					print(f"Violated agreement. Porcess {proc} has delivered message {message} but process {prc} did not.")
					return False
	return True


def check_fifo(proc_messages):
	for proc, messages in proc_messages.items():
		for i, message in enumerate(messages):
			if message.startswith("d"):
				sender, content = message.split(" ")[1:]
				for j in range(i):
					if messages[j].startswith("d"):
						sndr, cont = messages[j].split(" ")[1:]
						if sender == sndr and int(cont) >= int(content):
							print(f"Violated fifo on process {proc}")
							return False
	return True


def check_no_bullshit(proc_messages, n_processes, n_messages):
	for proc, messages in proc_messages.items():
		for message in messages:
			if message.startswith("d"):
				parts = message.split(" ")
				if not (1 <= int(parts[1]) <= n_processes) or not (1 <= int(parts[2]) <= n_messages):
					print(f"Bullshit in {proc}")
			elif message.startswith("b"):
				parts = message.split(" ")
				if not (1 <= int(parts[1]) <= n_messages):
					print(f"Bullshit in {proc}")
			else:
				print(f"Bullshit in {proc}")


def count_total_delivered(proc_messages):
	total = 0
	for proc, messages in proc_messages.items():
		total += len(list(filter(lambda m: m.startswith("d"), messages)))
	print(f"Total messages delivered: {total}" )


def main():
	

	n_messages = -1
	with open(os.path.join(log_folder, 'config')) as f:
		n_messages = int(f.read().splitlines()[0].split()[0])

	n_processes = -1
	with open(os.path.join(log_folder, 'hosts')) as f:
		n_processes = len(f.read().splitlines())

	proc_messages = get_messages()

	check_errors()
	check_no_creation(proc_messages, n_messages)
	check_no_duplication(proc_messages)
	check_uniform_agreement(proc_messages)
	check_fifo(proc_messages)
	check_no_bullshit(proc_messages, n_processes, n_messages)

	count_total_delivered(proc_messages)


if __name__ == '__main__':
	main()