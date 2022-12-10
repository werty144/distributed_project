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
					return False
			elif message.startswith("b"):
				parts = message.split(" ")
				if not (1 <= int(parts[1]) <= n_messages):
					print(f"Bullshit in {proc}")
					return False
			else:
				print(f"Bullshit in {proc}")
				return False


def count_total_delivered(proc_messages):
	total = 0
	for proc, messages in proc_messages.items():
		total += len(list(filter(lambda m: m.startswith("d"), messages)))
	print(f"Total messages delivered: {total}" )


def get_proc_names():
	names = set()
	for file in os.listdir(log_folder):
		if file.endswith('.output'):
			process_name = file.split('.')[0]
			names.add(process_name)
	return names


def get_proposals(name):
	proposals = []
	for file in os.listdir(log_folder):
		if file.endswith(f'{name}.config'):
			with open(os.path.join(log_folder, file)) as f:
				lines = f.read().splitlines()
				for line in lines[1:]:
					proposals.append([int(x) for x in line.split()])
	return proposals
			

def get_decisions(name):
	decisions = []
	for file in os.listdir(log_folder):
		if file.endswith(f'{name}.output'):
			with open(os.path.join(log_folder, file)) as f:
				lines = f.read().splitlines()
				for line in lines:
					decisions.append([int(x) for x in line.split()])
	return decisions


def check_validity(names, proposals, decisions):
	for name in names:
		for i, prop in enumerate(proposals[name]):
			dec = decisions[name][i]
			subset_check = all(x in dec for x in prop)
			if not subset_check:
				print(f"Violated validity self-subset at {name} round {i + 1}")

			props_union = []
			for nm in names:
				props_union += proposals[nm][i]
			
			superset_check = all(x in props_union for x in dec)
			if not superset_check:
				print(f"Violated validity superset at {name} round {i + 1}")


def check_consistency(names, proposals, decisions):
	for name in names:
		for i, prop in enumerate(proposals[name]):
			for other in names:
				consistency_check = all(x in decisions[other][i] for x in decisions[name][i]) or all(x in decisions[name][i] for x in decisions[other][i])
				if not consistency_check:
					print(f"Violated consistency for {name} and {other} at round {i + 1}")


def main():


	n_processes = -1
	with open(os.path.join(log_folder, 'hosts')) as f:
		n_processes = len(f.read().splitlines())

	names = get_proc_names()
	proposals = {name: get_proposals(name) for name in names}
	decisions = {name: get_decisions(name) for name in names}

	check_validity(names, proposals, decisions)
	check_consistency(names, proposals, decisions)





if __name__ == '__main__':
	main()