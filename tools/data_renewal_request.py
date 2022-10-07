#!/usr/bin/env python3
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

"""
A script to help generate a data review request comment and to update metrics.yaml
with results of renewal requests.
"""

import csv
import sys
import yaml

from yaml.loader import FullLoader


class MyDumper(yaml.Dumper):

    def increase_indent(self, flow=False, indentless=False):
        return super(MyDumper, self).increase_indent(flow, False)

# This will preserve multiline strings from the original yaml document
def str_presenter(dumper, data):
    desc_indent_len = 4
    # skip data review strings
    # if ("https://github" in data):
    #   return dumper.represent_scalar('tag:yaml.org,2002:str', data)
    if '\n' in data:  # check for multiline string
        return dumper.represent_scalar('tag:yaml.org,2002:str', data, style='|')
    # elif len(data) > 80 - desc_indent_len - len("description"):
    #     # updated_data = f'\n{data}'
    #     return dumper.represent_scalar('tag:yaml.org,2002:str', data, style='|')
    return dumper.represent_scalar('tag:yaml.org,2002:str', data)

yaml.add_representer(str, str_presenter)

try:
    version = sys.argv[1]
    new_data_review = sys.argv[2]
except:
    print ("usage is to include arguments of the form <version> <new data review URL>")
    quit()

expiry_filename = version + "_expiry_list.csv"
filled_renewal_filename = version + "_filled_renewal_request.txt"

yaml_file = yaml.load(open('../app/metrics.yaml', 'r'), Loader=FullLoader)

expiry_cvs_file = open(expiry_filename, 'r')
csv_reader = csv.DictReader(expiry_cvs_file)
output_string = ""
total_count = 0
updated_version = int(version) + 13
for row in csv_reader:
    (section, metric) = row["name"].split('.')
    if row["keep(Y/N)"] == 'n':
        yaml_file[section].pop(metric)
        continue
    total_count += 1
    output_string += f'` {row["name"]}`\n'
    output_string += "1) Provide a link to the initial Data Collection Review Request for this collection.\n"
    output_string += f'    - {eval(row["data_reviews"])[0]}\n'
    output_string += "\n"
    output_string += "2) When will this collection now expire?\n"
    output_string += f'    - {updated_version}' + "\n"
    output_string += "\n"
    output_string += "3) Why was the initial period of collection insufficient?\n"
    output_string += f'    - {row["reason to extend"]}\n'
    output_string += "\n"
    output_string += "———\n"

    yaml_file[section][metric]["data_reviews"].append(new_data_review)
    yaml_file[section][metric]["expires"] = updated_version

header = "# Request for Data Collection Renewal\n"
header += "### Renew for 1 year\n"
header += f'Total: {total_count}\n'
header += "———\n\n"

# yaml.dump(yaml_file, default_flow_style=False, sort_keys=False)
with open("new_metrics.yaml", 'w+') as outfile:
    outfile.write("""# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
---
    """)
    yaml.dump(yaml_file, Dumper=MyDumper, stream=outfile, sort_keys=False)
